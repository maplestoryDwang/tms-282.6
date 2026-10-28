package net.swordie.ms.world.tms282.newbie.steps;

import net.swordie.ms.world.tms282.newbie.NewbieJson;
import net.swordie.ms.world.tms282.newbie.NewbieRun;
import net.swordie.ms.world.tms282.newbie.NewbieStep;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code StoryDialog}: a few pages of text (say with "next"; no cut scene, no direction mode). Settings:
 * {@code pages} (1..6) and {@code byLoginJob} (LoginJob name -> its own pages, e.g. {@code KINESIS}).
 */
public final class StoryDialogStep implements NewbieStep {
    public static final String TYPE = "StoryDialog";

    private final String id;
    private final List<String> pages;
    private final Map<String, List<String>> byLoginJob;

    StoryDialogStep(String id, List<String> pages, Map<String, List<String>> byLoginJob) {
        this.id = id;
        this.pages = List.copyOf(pages);
        this.byLoginJob = Map.copyOf(byLoginJob);
    }

    public static StoryDialogStep read(String id, NewbieJson s) {
        List<String> pages = s.texts("pages", 1, 6);
        noMenu(s.path + ".pages", pages);
        Map<String, List<String>> by = new LinkedHashMap<>();
        for (Map.Entry<String, NewbieJson> e : s.entries("byLoginJob").entrySet()) {
            try { net.swordie.ms.constants.JobConstants.LoginJob.valueOf(e.getKey()); }
            catch (IllegalArgumentException ex) { throw NewbieJson.invalid(e.getValue().path, "unknown LoginJob"); }
            if (!e.getKey().matches("[A-Z_0-9]{1,32}")) throw NewbieJson.invalid(e.getValue().path, "LoginJob name expected (e.g. KINESIS)");
            List<String> p = e.getValue().asTexts(1, 6);
            noMenu(e.getValue().path, p);
            by.put(e.getKey(), p);
        }
        return new StoryDialogStep(id, pages, by);
    }

    /** A page with {@code #L} would turn into a menu (ScriptManagerImpl.sendGeneralSay). */
    private static void noMenu(String where, List<String> pages) {
        for (String p : pages) {
            if (p.contains("#L")) throw NewbieJson.invalid(where, "a story page must not contain #L (menu)");
        }
    }

    public List<String> pagesFor(String loginJob) {
        return byLoginJob.getOrDefault(loginJob, pages);
    }

    public Map<String, List<String>> byLoginJob() {
        return byLoginJob;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Outcome run(NewbieRun run) {
        List<String> p = pagesFor(run.progress().loginJob());
        for (String page : p) {
            run.dialog().next(page);
        }
        return Outcome.NEXT;
    }
}
