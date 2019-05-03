package tasks;

import miscellaneous.Utils;

import java.util.concurrent.Callable;

/**
 * Search for regexp matches in text.
 */
public class SearchInTextTask implements Callable<Integer> {

    final private String text;
    final private String regExp;

    public SearchInTextTask(String text, String regExp) {
        this.text = text;
        this.regExp = regExp;
    }

    @Override
    public Integer call() {
        return Utils.countMatches(text, regExp);
    }

}
