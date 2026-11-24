package com.lavrel.assistant.domain;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which wiki licenses allow Lavrel to reuse the text: Creative Commons Attribution-ShareAlike (CC BY-SA), the license
 * of Fandom, wiki.gg and most game wikis. Non-commercial ones (CC BY-NC-SA) are refused, and so is anything else.
 */
public final class WikiLicenses {

    private static final Pattern SHARE_ALIKE = Pattern.compile("by-sa|cc-by-sa|share.?alike");
    private static final Pattern NON_COMMERCIAL = Pattern.compile("by-nc|non.?commercial|\\bnc\\b");
    private static final Pattern VERSION = Pattern.compile("by-sa/(\\d\\.\\d)|by-sa[ -](\\d\\.\\d)");

    private WikiLicenses() {
    }

    /**
     * @return the license to show under answers, e.g. "CC BY-SA 3.0"
     * @throws WikiLicenseNotAllowedException if the wiki's license does not allow reuse, or it declares none
     */
    public static String allowed(WikiSiteInfo info) {
        String url = info.licenseUrl() == null ? "" : info.licenseUrl();
        String text = info.licenseText() == null ? "" : info.licenseText();
        String both = (url + " " + text).toLowerCase(Locale.ROOT);
        String declared = text.isBlank() ? url : text;
        if (both.isBlank()) {
            throw new WikiLicenseNotAllowedException("The wiki does not declare a license");
        }
        if (NON_COMMERCIAL.matcher(both).find() || !SHARE_ALIKE.matcher(both).find()) {
            throw new WikiLicenseNotAllowedException(
                    "The wiki's license is \"" + declared.trim() + "\"; only CC BY-SA (not non-commercial) is allowed");
        }
        Matcher version = VERSION.matcher(both);
        if (version.find()) {
            return "CC BY-SA " + (version.group(1) != null ? version.group(1) : version.group(2));
        }
        return "CC BY-SA";
    }
}
