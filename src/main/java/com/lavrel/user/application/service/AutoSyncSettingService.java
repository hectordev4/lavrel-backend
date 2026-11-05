package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.AutoSyncSettingUseCase;
import com.lavrel.user.application.port.out.UserActivityPort;
import org.springframework.stereotype.Service;

@Service
public class AutoSyncSettingService implements AutoSyncSettingUseCase {

    private final UserActivityPort activity;

    public AutoSyncSettingService(UserActivityPort activity) {
        this.activity = activity;
    }

    @Override
    public boolean isEnabled(long userId) {
        return activity.autoSyncEnabled(userId);
    }

    @Override
    public void setEnabled(long userId, boolean enabled) {
        activity.setAutoSync(userId, enabled);
    }
}
