package com.lavrel.shared.web;

import com.lavrel.shared.domain.Platform;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Lets URLs use lowercase platform names (/accounts/steam/link) for Platform path variables and parameters.
 * Spring Boot registers Converter beans with MVC automatically. An unknown name becomes a 400 invalid_parameter.
 */
@Component
public class PlatformPathConverter implements Converter<String, Platform> {

    @Override
    public Platform convert(String source) {
        return Platform.valueOf(source.trim().toUpperCase(Locale.ROOT));
    }
}
