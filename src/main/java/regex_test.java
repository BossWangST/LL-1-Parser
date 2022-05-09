import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class regex_test {
    public static void main(String[] args) {
        Pattern p = Pattern.compile("^\\{\\[\s*(.*?)\s*\\]\\}");
        Matcher m = p.matcher("{[{1,PUNCTUATION,LSBR}{1,PUNCTUATION,RSBR}]}");
        if (m.find()) {
            System.out.println(m.group(1));
        } else System.out.println("NOT FOUND");
    }
}
