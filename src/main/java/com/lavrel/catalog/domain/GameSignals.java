package com.lavrel.catalog.domain;

import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import java.util.List;

/**
 * What the policy looks at. Any field may be null or empty when that data has not been fetched yet,
 * which lets the service pre-screen on tags alone before spending an appdetails call.
 */
public record GameSignals(
        long appId,
        String type,
        List<Integer> contentDescriptorIds,
        List<TagWeight> tags,
        List<String> genres) {
}
