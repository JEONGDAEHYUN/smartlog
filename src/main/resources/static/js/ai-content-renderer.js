/**
 * AI 결과 텍스트 렌더러
 * - Gemini 가 생성한 텍스트("■ 섹션", "1. 항목", "[HH:MM~HH:MM]" 등)를
 *   카드/배지/들여쓰기가 적용된 HTML 로 변환한다.
 * - XSS 방지: 모든 사용자 텍스트는 escapeHtml() 처리 후 삽입.
 * - 사용법:
 *     <div id="ai-result" data-raw="..."></div>
 *     SmartLogAi.render('ai-result');
 *   또는
 *     SmartLogAi.renderTo(elementRef, rawText);
 */
(function (window) {
    'use strict';

    // 섹션명(■ 다음 텍스트) → 이모지 + 헤더 색 매핑
    // 매칭 시 includes() 로 부분 일치 검사 (예: "주요 업무 내역" 도 "업무" 로 매칭)
    var SECTION_STYLE = [
        { match: ['수행 업무', '업무 내역', '주요 업무', '업무'],         icon: '🗂', bg: '#e7f0fa', fg: '#1f4e79' },
        { match: ['요약', '개요'],                                         icon: '📌', bg: '#e8f5e9', fg: '#1a7a4a' },
        { match: ['통계', '현황', '실적'],                                  icon: '📊', bg: '#f3e8ff', fg: '#7c3aed' },
        { match: ['특이사항', '이슈', '문제'],                              icon: '⚠️', bg: '#fff4e5', fg: '#b7770d' },
        { match: ['권고', '제안', '개선', '향후', '계획'],                  icon: '💡', bg: '#fffbe6', fg: '#9a7d0a' },
        { match: ['비고', '참고', '메모'],                                  icon: '📝', bg: '#f4f6f8', fg: '#566573' },
        { match: ['제목', '타이틀'],                                        icon: '📋', bg: '#e3f2fd', fg: '#1565c0' }
    ];
    var DEFAULT_STYLE = { icon: '📄', bg: '#f4f6f8', fg: '#566573' };

    function escapeHtml(s) {
        if (s == null) return '';
        return String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function styleFor(name) {
        if (!name) return DEFAULT_STYLE;
        for (var i = 0; i < SECTION_STYLE.length; i++) {
            var s = SECTION_STYLE[i];
            for (var j = 0; j < s.match.length; j++) {
                if (name.indexOf(s.match[j]) !== -1) return s;
            }
        }
        return DEFAULT_STYLE;
    }

    // 시간 badge 공통 마크업
    function timeBadge(text) {
        return '<span style="display:inline-block;padding:1px 7px;margin:0 3px 0 0;' +
               'background:#e3f2fd;color:#1565c0;border-radius:4px;font-size:11px;' +
               'font-weight:600;font-family:monospace;">' + text + '</span>';
    }

    // 한 줄 안의 시간 패턴을 파란 badge 로 변환
    // 지원 형태: [HH:MM~HH:MM] (구간) / [HH:MM~] (종료시간 없음) / [HH:MM] (단일 시각)
    // 종료시간(END_DT)이 없을 때 UI 가 깨지지 않도록 세 경우를 모두 처리한다.
    // 상태(진행중/완료 등)는 시간으로 단정하지 않고 시간만 표기한다.
    function highlightTimeRange(escapedLine) {
        return escapedLine.replace(
            /\[\s*(\d{1,2}:\d{2})\s*(?:[~\-]\s*(\d{1,2}:\d{2})?)?\s*\]/g,
            function (_, start, end) {
                if (end) return timeBadge(start + '~' + end);   // 정상 구간
                // 종료시간 없음: 시작 시각만 표기 (상태 추정 문구 없음)
                return timeBadge(start);
            }
        );
    }

    // 본문 한 줄 → HTML 변환 (번호 / 하이픈 / 일반)
    function renderLine(rawLine) {
        var line = rawLine.replace(/\s+$/, '');
        if (line === '' || line === '-' || line === '없음') {
            return null; // 단일 하이픈/공백 줄은 호출부에서 "해당 사항 없음" 으로 처리
        }

        var escaped = escapeHtml(line.replace(/^\s+/, ''));
        escaped = highlightTimeRange(escaped);

        // 1. / 2. ... 번호 매김
        var numMatch = /^(\d+)\.\s*(.+)$/.exec(line.replace(/^\s+/, ''));
        if (numMatch) {
            var num = escapeHtml(numMatch[1]);
            var body = highlightTimeRange(escapeHtml(numMatch[2]));
            return (
                '<div style="display:flex;gap:8px;padding:6px 0;border-bottom:1px dashed #eef0f3;">' +
                  '<div style="flex-shrink:0;width:22px;height:22px;border-radius:50%;background:#e9ecef;' +
                  'color:#495057;font-size:11px;font-weight:700;display:flex;align-items:center;' +
                  'justify-content:center;">' + num + '</div>' +
                  '<div style="flex:1;font-size:13px;line-height:1.6;color:#212529;">' + body + '</div>' +
                '</div>'
            );
        }

        // - 항목
        var bulletMatch = /^[-•]\s*(.+)$/.exec(line.replace(/^\s+/, ''));
        if (bulletMatch) {
            var bbody = highlightTimeRange(escapeHtml(bulletMatch[1]));
            return (
                '<div style="display:flex;gap:8px;padding:4px 0;">' +
                  '<div style="flex-shrink:0;color:#8a9bb0;font-weight:700;">•</div>' +
                  '<div style="flex:1;font-size:13px;line-height:1.6;color:#212529;">' + bbody + '</div>' +
                '</div>'
            );
        }

        // 일반 문장
        return '<div style="font-size:13px;line-height:1.7;color:#212529;padding:2px 0;">' + escaped + '</div>';
    }

    // 텍스트 전체 → 섹션 단위로 묶어 카드 HTML 생성
    function build(rawText) {
        if (!rawText || !rawText.trim()) {
            return '<div style="color:#8a9bb0;font-size:13px;padding:20px;text-align:center;">' +
                   '내용이 없습니다.</div>';
        }

        var lines = rawText.split(/\r?\n/);
        var sections = []; // { name, lines: [] }
        var current = null;

        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            // 섹션 헤더: "■ XXX" 또는 "## XXX" 또는 "▣ XXX"
            var headerMatch = /^\s*[■▣]\s*(.+?)\s*$/.exec(line) || /^\s*##\s*(.+?)\s*$/.exec(line);
            if (headerMatch) {
                if (current) sections.push(current);
                current = { name: headerMatch[1].trim(), lines: [] };
            } else {
                if (!current) {
                    current = { name: '', lines: [] }; // 헤더 없는 선두 텍스트
                }
                current.lines.push(line);
            }
        }
        if (current) sections.push(current);

        // 빈 섹션 + 선두 빈 섹션 정리
        sections = sections.filter(function (s) {
            return s.name || s.lines.some(function (l) { return l.trim() !== ''; });
        });

        if (sections.length === 0) {
            // 섹션 없는 평문 — 단일 카드로
            sections = [{ name: '', lines: lines }];
        }

        var html = '';
        for (var k = 0; k < sections.length; k++) {
            var sec = sections[k];
            var style = styleFor(sec.name);

            // 본문 줄 렌더링
            var bodyHtml = '';
            var hasContent = false;
            for (var m = 0; m < sec.lines.length; m++) {
                var rendered = renderLine(sec.lines[m]);
                if (rendered != null) {
                    bodyHtml += rendered;
                    hasContent = true;
                }
            }
            if (!hasContent) {
                // 내용 없는 섹션(비고 등)은 "없음" 으로 간결하게 표시
                bodyHtml =
                    '<div style="color:#8a9bb0;font-size:12px;font-style:italic;padding:8px 0;">' +
                    '없음</div>';
            }

            html +=
                '<div style="border:1px solid #e9ecef;border-radius:8px;margin-bottom:14px;' +
                'overflow:hidden;background:#fff;box-shadow:0 1px 2px rgba(0,0,0,0.03);">';

            if (sec.name) {
                html +=
                    '<div style="background:' + style.bg + ';color:' + style.fg + ';' +
                    'padding:10px 14px;font-weight:700;font-size:13px;' +
                    'border-bottom:1px solid #e9ecef;display:flex;align-items:center;gap:6px;">' +
                    '<span style="font-size:15px;">' + style.icon + '</span>' +
                    '<span>' + escapeHtml(sec.name) + '</span>' +
                    '</div>';
            }
            html +=
                '<div style="padding:12px 16px;">' + bodyHtml + '</div>' +
                '</div>';
        }
        return html;
    }

    function renderTo(target, rawText) {
        var el = (typeof target === 'string') ? document.getElementById(target) : target;
        if (!el) return;
        el.innerHTML = build(rawText);
    }

    function render(targetId) {
        var el = document.getElementById(targetId);
        if (!el) return;
        var raw = el.dataset.raw || el.textContent || '';
        el.innerHTML = build(raw);
    }

    window.SmartLogAi = { render: render, renderTo: renderTo, build: build };
})(window);