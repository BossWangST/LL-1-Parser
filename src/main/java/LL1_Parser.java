import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class symbol {
    int type;//0 <-> Non-Terminals  1 <-> Terminals -1 <-> Empty
    int feature;//0 <-> normal  1 <-> repeated  2 <-> optional  !3 <-> optional but repeated(only one)  !4 <-> repeated but optional(only one)
    String non_Terminal_name;
    int Terminal;//we only need to check if the token_id is correct
    int further_feature;//this is for tokens like RELOP etc.

    symbol(int type, int feature) {
        this.type = type;
        this.feature = feature;
    }

    symbol(int type, int feature, String non_Terminal_name) {
        this(type, feature);
        this.non_Terminal_name = non_Terminal_name;
        this.Terminal = -1;
    }

    symbol(int type, int feature, int id) {
        this(type, feature);
        this.non_Terminal_name = null;
        this.Terminal = id;
    }

    symbol(int type, int feature, int id, int further_feature) {
        this(type, feature, id);
        this.further_feature = further_feature;
    }
}

class rule {
    ArrayList<symbol> body;

    rule(ArrayList<symbol> body) {
        this.body = body;
    }
}

public class LL1_Parser {
    HashMap<String, rule[]> rules;//String <-> head, rule <-> body
    HashMap<String, Integer> token_id;
    BufferedReader reader;

    void insert_single_symbol(String r, ArrayList<symbol> current_body, int feature) {
        String[] pair = r.split(",");
        int identifier = Integer.valueOf(pair[0]);
        if (identifier == 0) {//it is a non-terminal
            current_body.add(new symbol(identifier, feature, pair[1]));
        } else if (identifier == 1) {//it is a terminal
            if (pair.length > 2) {//token with index
                current_body.add(new symbol(identifier, feature, token_id.get(pair[1]), token_id.get(pair[2])));
            } else {//token
                current_body.add(new symbol(identifier, feature, token_id.get(pair[1])));
            }
        } else {//empty
            current_body.add(new symbol(identifier, -1));
        }
    }

    void insert_symbol(String r, ArrayList<symbol> current_body, int feature) {
        switch (feature) {
            case 0://normal pair
                insert_single_symbol(r, current_body, feature);
                break;
            case 1://repeated
            case 2://optional
            case 3://optional_but_repeated
            case 4://repeated_but_optional
                Pattern p = Pattern.compile("^\\{(.*?)\\}");
                while (!r.equals("")) {
                    Matcher m = p.matcher(r);
                    if (m.find()) {
                        String current_single = m.group(1);
                        insert_single_symbol(current_single, current_body, feature);
                        r = r.substring(m.group(0).length(), r.length());
                    }
                }
                break;
            default:
                break;
        }
    }

    rule[] get_rules(String rules) {
        String[] all_rule = rules.split("\\$");
        Pattern p_optional = Pattern.compile("^\\{\\[\s*(.*?)\s*\\]\\}");
        Pattern p_repeated = Pattern.compile("^\\{{2}\s*(.*?)\s*\\}{2}");
        Pattern p_normal = Pattern.compile("^\\{\s*(.*?)\s*\\}");
        String[] pair;
        rule[] res = new rule[all_rule.length];
        int index = 0;
        for (String str : all_rule) {
            var current_body = new ArrayList<symbol>();
            //Here we have 3 options: normal | repeated | optional
            //We need to check it one by one
            while (!str.equals("")) {
                Matcher m_normal = p_normal.matcher(str);
                Matcher m_repeated = p_repeated.matcher(str);
                Matcher m_optional = p_optional.matcher(str);
                if (m_repeated.find()) {
                    String current_symbol = m_repeated.group(1);
                    Pattern p_repeated_optional = Pattern.compile("^\\{\\[\s*(.*?)\s*\\]\\}");
                    Matcher m_repeated_optional = p_repeated_optional.matcher(current_symbol);
                    if (m_repeated_optional.find()) {
                        insert_symbol(m_repeated_optional.group(1), current_body, 4);
                    } else {
                        insert_symbol(current_symbol, current_body, 1);
                    }
                    str = str.substring(m_repeated.group(0).length(), str.length());
                } else if (m_optional.find()) {
                    String current_symbol = m_optional.group(1);
                    Pattern p_optional_repeated = Pattern.compile("^\\{\\{\s*(.*?)\s*\\}\\}");
                    Matcher m_optional_repeated = p_optional_repeated.matcher(current_symbol);
                    if (m_optional_repeated.find()) {
                        insert_symbol(m_optional_repeated.group(1), current_body, 3);
                    } else {
                        insert_symbol(current_symbol, current_body, 2);
                    }
                    str = str.substring(m_optional.group(0).length(), str.length());
                } else if (m_normal.find()) {
                    String current_symbol = m_normal.group(1);
                    insert_symbol(current_symbol, current_body, 0);
                    str = str.substring(m_normal.group(0).length(), str.length());
                } else {
                    System.out.println("Wrong rule --->" + str);
                    break;
                }
            }
            res[index++] = new rule(current_body);
        }
        return res;
    }

    public LL1_Parser(BufferedReader reader, Lexical_Analysis lexical) throws IOException {
        this.reader = reader;
        this.token_id = new HashMap<String, Integer>();
        this.rules = new HashMap<String, rule[]>();
        String current_line = "";
        int flag = 0;
        while (true) {
            current_line = reader.readLine();
            if (current_line == null)
                break;
            if (current_line.equals("") || current_line.charAt(0) == '#') continue;
            if (current_line.contains("%%")) {
                flag++;
                continue;
            }
            if (flag == 0) {
                String[] token_name = current_line.split("\s+");
                token_id.put(token_name[0], Integer.valueOf(token_name[1]));
            } else {
                System.out.println(current_line);
                String[] current_rule = current_line.split("\s+", 2);
                rule[] current_rules = get_rules(current_rule[1]);
                this.rules.put(current_rule[0].substring(1, current_rule[0].length() - 1), current_rules);
            }
        }
    }

    public static void main(String[] args) {
        try {
            var lex_reader = new BufferedReader(new InputStreamReader(new FileInputStream("test.c")));
            var lexical = new Lexical_Analysis(lex_reader);
            var token_sequence = lexical.scanner();
            System.out.println(token_sequence.toString());
            var grammar_reader = new BufferedReader(new InputStreamReader(new FileInputStream("grammar.txt")));
            var parser = new LL1_Parser(grammar_reader, lexical);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
