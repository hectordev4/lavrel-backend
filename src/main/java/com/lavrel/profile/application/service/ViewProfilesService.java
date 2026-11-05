package com.lavrel.profile.application.service;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase;
import com.lavrel.profile.application.port.out.PublicProfileQueryPort;
import com.lavrel.profile.domain.ProfileNotFoundException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ViewProfilesService implements ViewProfilesUseCase {

    static final int PAGE_SIZE = 20;
    private static final Pattern PUBLIC_ID = Pattern.compile("\\d{16}");

    private final PublicProfileQueryPort profiles;

    public ViewProfilesService(PublicProfileQueryPort profiles) {
        this.profiles = profiles;
    }

    @Override
    public ProfileHeader header(long publicId) {
        return profiles.findHeader(publicId).orElseThrow(ProfileNotFoundException::new);
    }

    @Override
    public List<ProfileHeader> search(String query, int page) {
        String q = query.trim();
        Long exactId = PUBLIC_ID.matcher(q).matches() ? Long.valueOf(q) : null;
        return profiles.search(q, exactId, PAGE_SIZE, (long) Math.max(0, page) * PAGE_SIZE);
    }
}
