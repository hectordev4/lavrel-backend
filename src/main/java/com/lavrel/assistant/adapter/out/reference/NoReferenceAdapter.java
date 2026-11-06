package com.lavrel.assistant.adapter.out.reference;

import com.lavrel.assistant.application.port.out.ReferencePort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.Excerpt;
import java.util.List;
import org.springframework.stereotype.Component;

/** No reference sources yet: every answer comes from the model's own knowledge and is marked as not grounded. */
@Component
public class NoReferenceAdapter implements ReferencePort {

    @Override
    public List<Excerpt> find(AchievementContext achievement) {
        return List.of();
    }
}
