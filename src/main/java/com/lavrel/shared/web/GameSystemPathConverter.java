package com.lavrel.shared.web;

import com.lavrel.catalog.domain.GameSystem;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Lowercase system names in URLs and parameters (pc, xbox, playstation). Unknown names are a 400. */
@Component
public class GameSystemPathConverter implements Converter<String, GameSystem> {

    @Override
    public GameSystem convert(String source) {
        return GameSystem.valueOf(source.trim().toUpperCase(Locale.ROOT));
    }
}
