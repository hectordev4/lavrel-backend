package com.lavrel.catalog.domain;

import com.lavrel.catalog.domain.GameMetadata.FeatureGroup;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The store features the frontend can show or hide. Each one is exposed as a boolean under its apiKey.
 * Ids are Steam's own category ids, taken from real store data. Steam categories that are not listed
 * here are still stored and returned in the full feature list, just without a dedicated boolean.
 */
public enum GameFeature {

    SINGLE_PLAYER(2, "singlePlayer", FeatureGroup.PLAYER_MODE),
    MULTI_PLAYER(1, "multiPlayer", FeatureGroup.PLAYER_MODE),
    CO_OP(9, "coOp", FeatureGroup.PLAYER_MODE),
    ONLINE_CO_OP(38, "onlineCoOp", FeatureGroup.PLAYER_MODE),
    CROSS_PLATFORM_MULTIPLAYER(27, "crossPlatformMultiplayer", FeatureGroup.PLAYER_MODE),

    STEAM_ACHIEVEMENTS(22, "steamAchievements", FeatureGroup.FEATURE),
    STEAM_TRADING_CARDS(29, "steamTradingCards", FeatureGroup.FEATURE),
    STEAM_CLOUD(23, "steamCloud", FeatureGroup.FEATURE),
    HDR_AVAILABLE(61, "hdrAvailable", FeatureGroup.FEATURE),
    FAMILY_SHARING(62, "familySharing", FeatureGroup.FEATURE),

    FULL_CONTROLLER_SUPPORT(28, "fullControllerSupport", FeatureGroup.CONTROLLER),
    DUALSHOCK_SUPPORT(55, "dualShockSupport", FeatureGroup.CONTROLLER),
    DUALSENSE_SUPPORT(57, "dualSenseSupport", FeatureGroup.CONTROLLER),
    STEAM_INPUT_API(59, "steamInputApi", FeatureGroup.CONTROLLER),

    ADJUSTABLE_TEXT_SIZE(64, "adjustableTextSize", FeatureGroup.ACCESSIBILITY),
    SUBTITLE_OPTIONS(65, "subtitleOptions", FeatureGroup.ACCESSIBILITY),
    COLOR_ALTERNATIVES(66, "colorAlternatives", FeatureGroup.ACCESSIBILITY),
    CAMERA_COMFORT(67, "cameraComfort", FeatureGroup.ACCESSIBILITY),
    CUSTOM_VOLUME_CONTROLS(68, "customVolumeControls", FeatureGroup.ACCESSIBILITY),
    STEREO_SOUND(69, "stereoSound", FeatureGroup.ACCESSIBILITY),
    SURROUND_SOUND(70, "surroundSound", FeatureGroup.ACCESSIBILITY),
    KEYBOARD_ONLY_OPTION(75, "keyboardOnlyOption", FeatureGroup.ACCESSIBILITY);

    private final int steamCategoryId;
    private final String apiKey;
    private final FeatureGroup group;

    GameFeature(int steamCategoryId, String apiKey, FeatureGroup group) {
        this.steamCategoryId = steamCategoryId;
        this.apiKey = apiKey;
        this.group = group;
    }

    public int steamCategoryId() {
        return steamCategoryId;
    }

    public String apiKey() {
        return apiKey;
    }

    public FeatureGroup group() {
        return group;
    }

    public static Optional<GameFeature> byCategoryId(int steamCategoryId) {
        return Stream.of(values()).filter(f -> f.steamCategoryId == steamCategoryId).findFirst();
    }
}
