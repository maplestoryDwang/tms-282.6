package net.swordie.ms.connection.api.launcher.news;

import net.swordie.ms.GameSettings;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@code game.slide} (the scrolling notice players get on every channel entry, hot reloaded by {@link GameSettings}):
 * when set, one notice {@code {id:"slide", kind:"up"}} with the slide text as its body.
 */
public final class SlideNewsSource implements NewsSource {
    public static final String ID = "slide";
    public static final String TITLE = "伺服器公告";

    private final Supplier<String> slide;

    /** The live settings. */
    public SlideNewsSource() {
        this(GameSettings::slide);
    }

    public SlideNewsSource(Supplier<String> slide) {
        this.slide = Objects.requireNonNull(slide, "slide");
    }

    @Override
    public String name() {
        return "slide";
    }

    @Override
    public List<NewsItem> items(long now) {
        String text = slide.get();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(new NewsItem(ID, "up", TITLE, text.strip(), null, null, false));
    }
}
