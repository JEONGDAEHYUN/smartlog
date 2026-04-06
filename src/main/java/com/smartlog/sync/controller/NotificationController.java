package com.smartlog.sync.controller;

import com.smartlog.sync.entity.mariadb.NotiInfo;
import com.smartlog.sync.entity.mariadb.UserInfo;
import com.smartlog.sync.repository.mariadb.NotiInfoRepository;
import com.smartlog.sync.repository.mariadb.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// 알림 센터 Controller
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotiInfoRepository notiInfoRepository;
    private final UserInfoRepository userInfoRepository;

    // 알림 목록
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) String filter, Model model) {
        UserInfo user = getUser(userDetails);
        if (user == null) return "redirect:/login";

        Long userId = user.getUserId();
        List<NotiInfo> notifications;

        if ("unread".equals(filter)) {
            notifications = notiInfoRepository.findByUserInfoUserIdAndIsRead(userId, "N");
        } else {
            notifications = notiInfoRepository.findByUserInfoUserId(userId);
        }

        long totalCount = notiInfoRepository.findByUserInfoUserId(userId).size();
        long unreadCount = notiInfoRepository.findByUserInfoUserIdAndIsRead(userId, "N").size();

        model.addAttribute("notifications", notifications);
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

    private UserInfo getUser(UserDetails userDetails) {
        return userInfoRepository.findByUserEmail(userDetails.getUsername()).orElse(null);
    }
}
