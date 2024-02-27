import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InputParser has one main static method, `parseInput`, which takes an input
 * string then tries to parse it and return the values. No input validation is
 * done.
 * 
 * @see #parseInput(String)
 */
final class InputParser {
    /**
     * Patterns are stored in raw regex string format. In each nested array, the
     * first string matches the command name part while the second string matches
     * the pattern for the parameters. If the second string is null, there are no
     * parameters for that command. If the pattern is not followed as-is, no match
     * will be found. Parts encased in parentheses are for capture groups.
     */
    private static String[][] patterns = {
            // only checks length and format; does not check if values are valid
            { "/(join)", "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|localhost) (\\d{1,5})" },

            { "/(leave)", null },

            // \w = `A-Za-z0-9_` so any alphanumeric character is accepted
            { "/(register)", "(\\w+)" },

            // don't validate file name; just try to open file later
            { "/(store)", "(.+)" },

            { "/(dir)", null },

            // don't validate file name; just try to open file later
            { "/(get)", "(.+)" },

            { "/(\\?)", null },

            // bonus feature: unicast; allows user to message someone else in server
            { "/(msg)", "(\\w+) (.+)" },

            // bonus feature: broadcast; allows user to message everyone else in server
            { "/(bc)", "(.+)" },
    };

    static {
        // prioritize longer strings which might have substrings that are also commands
        // e.g. /joinchat should be checked first before /join since the matcher tries
        // to match the command at the beginning of the string
        Arrays.sort(patterns, (a, b) -> b[0].compareTo(a[0]));
    }

    /**
     * Takes an input string then returns an InputParser object that contains the
     * parsed command, parameters, and error message, if any, obtainable using
     * getters. Only the format is loosely checked, but the values itself may be
     * not exist or be incorrect.
     * 
     * @param s Raw input string to parse.
     * @return
     *         InputParser object with three instance variables (all can be null):
     *         1. command (String): Name of the command used.
     *         2. params (ArrayList<String>): ArrayList of parameters passed.
     *         3. errorMessage (String): Message detailing what went wrong.
     */
    public static InputParser parseInput(String s) {
        InputParser ip = new InputParser();
        for (int i = 0; i < patterns.length; i++) {
            // create pattern for command name
            StringBuilder sb = new StringBuilder();
            sb.append("^"); // match start of string
            sb.append(patterns[i][0]);
            Pattern p = Pattern.compile(sb.toString());

            // if command name is not present, try other command names
            Matcher m = p.matcher(s);
            if (!m.find()) {
                continue;
            }

            // get command name
            ip.setCommand(m.group(1));

            // create pattern for command name + params (if any)
            if (patterns[i][1] != null) {
                sb.append(" ");
                sb.append(patterns[i][1]);
            }
            sb.append("$"); // match end of string
            p = Pattern.compile(sb.toString());
            m = p.matcher(s);

            // if input does not match command syntax, abort
            if (!m.matches()) {
                ip.setErrorMessage("Command parameters do not match or is not allowed.");
                break;
            }

            // get command parameters
            ArrayList<String> params = new ArrayList<>();
            for (int j = 2; j <= m.groupCount(); j++) {
                params.add(m.group(j));
            }
            ip.setParams(params);
            break;
        }

        // if no command was found, notify user
        if (ip.getCommand() == null) {
            ip.setErrorMessage("Command not found.");
        }

        return ip;
    }

    private String command;
    private ArrayList<String> params;
    private String errorMessage;

    private InputParser() {
    }

    public String getCommand() {
        return command;
    }

    public ArrayList<String> getParams() {
        return params;
    }

    public String getParams(int index) {
        return params.get(index);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private void setCommand(String command) {
        this.command = command;
    }

    private void setParams(ArrayList<String> params) {
        this.params = params;
    }

    private void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
