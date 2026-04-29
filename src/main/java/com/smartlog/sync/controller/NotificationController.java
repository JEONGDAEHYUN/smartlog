package com.smartlog.sync.controller;

import com.smartlog.sync.dto.NotiInfoDto;
import com.smartlog.sync.repository.entity.NotiInfo;
import com.smartlog.sync.repository.NotiInfoRepository;
import com.smartlog.sync.repository.entity.UserInfo;
import com.smartlog.sync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// 알림 센터 Controller
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotiInfoRepository notiInfoRepository;
    private final UserService userService;

    // 알림 목록
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) String filter, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        Long userId = user.getUserId();
        List<NotiInfo> notifications;

        if ("unread".equals(filter)) {
            notifications = notiInfoRepository.findByUserInfoUserIdAndIsSentAndIsRead(userId, "Y", "N");
        } else {
            notifications = notiInfoRepository.findByUserInfoUserIdAndIsSent(userId, "Y");
        }

        // 같은 일정(SCH_ID)의 알림은 최신 1개만 표시
        notifications = filterLatestPerSchedule(notifications);

        // Entity → DTO 변환 (View로는 DTO만 전달)
        List<NotiInfoDto> notificationDtos = NotiInfoDto.fromList(notifications);

        long totalCount = notificationDtos.size();
        long unreadCount = notificationDtos.stream().filter(n -> "N".equals(n.getIsRead())).count();

        model.addAttribute("notifications", notificationDtos);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("filter", filter);
        return "notification/list";
    }

    // 알림 읽음 처리
    @PostMapping("/read/{notiId}")
    public String markRead(@PathVariable Long notiId) {
        NotiInfo noti = notiInfoRepository.findById(notiId).orElse(null);
        if (noti != null) {
            noti.setIsRead("Y");
            notiInfoRepository.save(noti);
        }
        return "redirect:/notifications";
    }

    // 전체 읽음 처리
    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        List<NotiInfo> unreadList = notiInfoRepository.findByUserInfoUserIdAndIsRead(user.getUserId(), "N");
        unreadList.forEach(n -> n.setIsRead("Y"));
        notiInfoRepository.saveAll(unreadList);
        return "redirect:/notifications";
    }

    // 알림 삭제
    @PostMapping("/delete/{notiId}")
    public String delete(@PathVariable Long notiId) {
        notiInfoRepository.deleteById(notiId);
        return "redirect:/notifications";
    }

    // 같은 일정(SCH_ID)에 대해 최신 알림 1개만 유지 (Entity 단계에서 필터링)
    private List<NotiInfo> filterLatestPerSchedule(List<NotiInfo> notifications) {
        Map<Long, NotiInfo> latestMap = new LinkedHashMap<>();
        for (NotiInfo noti : notifications) {
            Long schId = noti.getSchInfo() != null ? noti.getSchInfo().getSchId() : null;
            if (schId == null) {
                // 일정 연결 없는 알림은 그대로 표시
                latestMap.put(-noti.getNotiId(), noti);
            } else {
                NotiInfo existing = latestMap.get(schId);
                if (existing == null || noti.getNotiDt().isAfter(existing.getNotiDt())) {
                    latestMap.put(schId, noti);
                }
            }
        }
        return new ArrayList<>(latestMap.values());
    }

    private UserInfo getUser(UserDetails userDetails) {
        return userService.getEntityByEmail(userDetails.getUsername());
    }
}
