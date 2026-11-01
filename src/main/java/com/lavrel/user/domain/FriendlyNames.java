package com.lavrel.user.domain;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Starting usernames like "SwiftOtter" or "SleepyWaffle": one adjective and one noun, picked at random from two
 * lists that always fit together. Friendlier than "player_k7x2qa9d", so people may well keep theirs.
 * The words are deliberately neutral (no real people, religion, politics, or anything that could combine into
 * an insult). Names repeat, which is fine: usernames are not unique, the public id tells people apart.
 * Every combination is a valid username (UsernamesTest checks all of them).
 */
public final class FriendlyNames {

    static final List<String> ADJECTIVES = List.of(
            "Swift", "Sleepy", "Brave", "Clever", "Cosmic", "Curious", "Dizzy", "Fancy", "Fluffy", "Fuzzy",
            "Gentle", "Giddy", "Glowing", "Grumpy", "Happy", "Hidden", "Humble", "Jolly", "Lucky", "Mighty",
            "Misty", "Nimble", "Noble", "Peppy", "Quiet", "Quirky", "Rapid", "Rusty", "Shiny", "Silent",
            "Silly", "Snappy", "Sneaky", "Spicy", "Sunny", "Tiny", "Turbo", "Wandering", "Wild", "Witty",
            "Zesty", "Bouncy", "Breezy", "Cheerful", "Crispy", "Dapper", "Electric", "Frosty", "Golden", "Lazy",
            "Mellow", "Pixelated", "Rocket", "Stormy", "Velvet", "Wobbly", "Arcane", "Epic", "Retro", "Neon");

    static final List<String> NOUNS = List.of(
            "Otter", "Panda", "Falcon", "Badger", "Koala", "Penguin", "Fox", "Lynx", "Raccoon", "Hedgehog",
            "Owl", "Gecko", "Walrus", "Narwhal", "Llama", "Yeti", "Dragon", "Phoenix", "Griffin", "Kraken",
            "Waffle", "Noodle", "Pickle", "Muffin", "Taco", "Dumpling", "Pancake", "Nacho", "Biscuit", "Donut",
            "Cactus", "Comet", "Meteor", "Nebula", "Pebble", "Acorn", "Mushroom", "Cloud", "Thunder", "Volcano",
            "Wizard", "Knight", "Ranger", "Pirate", "Ninja", "Robot", "Golem", "Goblin", "Bard", "Paladin",
            "Joystick", "Pixel", "Cartridge", "Controller", "Boss", "Sidekick", "Speedrunner", "Combo", "Respawn",
            "Checkpoint");

    private FriendlyNames() {
    }

    /** "Swift" + "Otter" = "SwiftOtter". The same random source gives the same name, so tests can pin it. */
    public static String compose(RandomGenerator random) {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        return adjective + noun;
    }
}
