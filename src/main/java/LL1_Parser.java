import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
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
        this.Terminal = -1;
        this.further_feature = -1;
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
    HashSet<String> contained_Non_Terminals;

    rule(ArrayList<symbol> body, HashSet<String> contained_Non_Terminals) {
        this.body = body;
        this.contained_Non_Terminals = contained_Non_Terminals;
    }
}

public class LL1_Parser {
    HashMap<String, rule[]> rules;//String <-> head, rule <-> body
    HashMap<String, Integer> token_id;
    BufferedReader reader;
    HashMap<String, HashSet<Integer>> first;
    HashMap<String, HashSet<Integer>> follow;
    HashMap<String, HashMap<Integer, Integer>> parser_table;


    void insert_single_symbol(String r, ArrayList<symbol> current_body, HashSet<String> current_Non_Terminals, int feature) {
        String[] pair = r.split(",");
        int identifier = Integer.valueOf(pair[0]);
        if (identifier == 0) {//it is a non-terminal
            current_body.add(new symbol(identifier, feature, pair[1]));
            current_Non_Terminals.add(pair[1]);
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

    void insert_symbol(String r, ArrayList<symbol> current_body, HashSet<String> current_Non_Terminals, int feature) {
        switch (feature) {
            case 0://normal pair
                insert_single_symbol(r, current_body, current_Non_Terminals, feature);
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
                        insert_single_symbol(current_single, current_body, current_Non_Terminals, feature);
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
        rule[] res = new rule[all_rule.length];
        int index = 0;
        for (String str : all_rule) {
            var current_body = new ArrayList<symbol>();
            var current_Non_Terminals = new HashSet<String>();
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
                        insert_symbol(m_repeated_optional.group(1), current_body, current_Non_Terminals, 4);
                    } else {
                        insert_symbol(current_symbol, current_body, current_Non_Terminals, 1);
                    }
                    str = str.substring(m_repeated.group(0).length());
                } else if (m_optional.find()) {
                    String current_symbol = m_optional.group(1);
                    Pattern p_optional_repeated = Pattern.compile("^\\{\\{\s*(.*?)\s*\\}\\}");
                    Matcher m_optional_repeated = p_optional_repeated.matcher(current_symbol);
                    if (m_optional_repeated.find()) {
                        insert_symbol(m_optional_repeated.group(1), current_body, current_Non_Terminals, 3);
                    } else {
                        insert_symbol(current_symbol, current_body, current_Non_Terminals, 2);
                    }
                    str = str.substring(m_optional.group(0).length());
                } else if (m_normal.find()) {
                    String current_symbol = m_normal.group(1);
                    insert_symbol(current_symbol, current_body, current_Non_Terminals, 0);
                    str = str.substring(m_normal.group(0).length());
                } else {
                    System.out.println("Wrong rule --->" + str);
                    break;
                }
            }
            res[index++] = new rule(current_body, current_Non_Terminals);
        }
        return res;
    }

    HashSet<Integer> get_single_first(String head, HashSet<String> Non_Terminal_first) {
        Non_Terminal_first.add(head);
        var current_first = new HashSet<Integer>();
        rule[] current_bodies = this.rules.get(head);
        for (rule r : current_bodies) {
            if (r.body.get(0).type == 1) {
                if (r.body.get(0).further_feature != -1) {//Note, here we need to clarify different further features
                    current_first.add(r.body.get(0).further_feature + r.body.get(0).Terminal);
                } else {
                    current_first.add(r.body.get(0).Terminal);
                }
            } else if (r.body.get(0).type == 0) {
                var needed_first = new HashSet<Integer>();
                if (this.first.containsKey(r.body.get(0).non_Terminal_name))
                    needed_first.addAll(this.first.get(r.body.get(0).non_Terminal_name));
                else
                    needed_first.addAll(get_single_first(r.body.get(0).non_Terminal_name, Non_Terminal_first));
                if (needed_first.contains(-1)) {
                    int index = 1;
                    while (needed_first.contains(-1)) {
                        needed_first.remove(-1);
                        HashSet<Integer> needed_next = new HashSet<>();
                        if (this.first.containsKey(r.body.get(index).non_Terminal_name))
                            needed_next.addAll(this.first.get(r.body.get(index++).non_Terminal_name));
                        else
                            needed_next.addAll(get_single_first(r.body.get(index++).non_Terminal_name, Non_Terminal_first));
                        needed_first.addAll(needed_next);
                        if (index == r.body.size() - 1)
                            break;
                    }
                }
                current_first.addAll(needed_first);
            } else {
                current_first.add(-1);
            }
        }
        this.first.put(head, current_first);
        System.out.println("First(" + head + ")=" + current_first.toString());
        return current_first;
    }

    void get_first_set() {
        var Non_Terminal_first = new HashSet<String>();
        for (String head : this.Non_Terminals) {
            if (Non_Terminal_first.contains(head)) continue;
            //var current_first = get_single_first(head, Non_Terminal_first);
            get_single_first(head, Non_Terminal_first);
        }
    }

    ArrayList<String> Non_Terminals;

    HashSet<Integer> get_single_follow(String current_rule_head, String head, rule ru, HashSet<String> Non_Terminal_follow, Boolean start_Non_Terminal) {
        Non_Terminal_follow.add(head);
        var current_follow = new HashSet<Integer>();
        var symbols = ru.body;
        for (int i = 0; i < symbols.size(); i++) {
            if (symbols.get(i).non_Terminal_name == null)//this is a Terminal
                continue;
            if (symbols.get(i).non_Terminal_name.equals(head)) {
                if (i == symbols.size() - 1) {//the Non-Terminal is in the end of the body
                    if (!Non_Terminal_follow.contains(current_rule_head)) {
                        for (String find_head : this.Non_Terminals) {
                            for (rule find_ru : rules.get(find_head)) {
                                if (find_ru.contained_Non_Terminals.contains(current_rule_head)) {
                                    get_single_follow(find_head, current_rule_head, find_ru, Non_Terminal_follow, start_Non_Terminal);
                                }
                            }
                        }
                    }
                    HashSet<Integer> needed_follow = follow.get(current_rule_head);
                    current_follow.addAll(needed_follow);
                } else {
                    if (symbols.get(i + 1).type == 1) {//if next symbol is Terminal
                        if (symbols.get(i + 1).further_feature != -1) {
                            current_follow.add(symbols.get(i + 1).Terminal + symbols.get(i + 1).further_feature);
                        } else {
                            current_follow.add(symbols.get(i + 1).Terminal);
                        }
                        break;
                    } else {//if next symbol is Non-Terminal
                        HashSet<Integer> needed_first = new HashSet<Integer>();
                        needed_first.addAll(this.first.get(symbols.get(i + 1).non_Terminal_name));
                        int index = 2;
                        if (needed_first.contains(-1)) {//if first(next non-terminal) has -1, then recursively add first set
                            while (needed_first.contains(-1)) {
                                needed_first.remove(-1);
                                current_follow.addAll(needed_first);
                                if (i + index < symbols.size()) {
                                    needed_first = new HashSet<Integer>();
                                    needed_first.addAll(this.first.get(symbols.get(i + index).non_Terminal_name));
                                } else {
                                    break;
                                }
                                index++;
                            }
                            if (needed_first.contains(-1)) current_follow.add(-1);
                        } else {
                            current_follow.addAll(needed_first);//or just add the first set to this follow set
                        }
                    }
                }
            }
        }
        this.follow.put(head, current_follow);
        return current_follow;
    }

    void get_follow_set() {
        var Non_Terminal_follow = new HashSet<String>();
        Boolean start_Non_Terminal = true;
        for (String head : this.Non_Terminals) {
            if (Non_Terminal_follow.contains(head)) continue;
            for (String r : this.Non_Terminals) {
                for (rule ru : rules.get(r)) {
                    if (ru.contained_Non_Terminals.contains(head)) {
                        //check all the body of rules containing this Non-Terminal
                        var current_follow = get_single_follow(r, head, ru, Non_Terminal_follow, start_Non_Terminal);
                        if (start_Non_Terminal) {
                            current_follow.add(-2);//start and end separator of a string
                            start_Non_Terminal = false;
                        }
                        System.out.println("Follow(" + head + ")=" + this.follow.get(head).toString());
                    }
                }
            }
        }

    }

    void get_parser_table() {
        //check all the rules of each Non-Terminal. Calculate SELECT set.
        for (String VN : this.Non_Terminals) {
            var current_select = new HashMap<Integer, Integer>();
            for (int i = 0; i < this.rules.get(VN).length; i++) {
                var r = this.rules.get(VN)[i];
                if (r.body.get(0).type == 1) {//this body begins with Terminal.
                    if (r.body.get(0).further_feature != -1) {
                        //if the next token is here the body's select, then choose i th rule to derivate
                        current_select.put(r.body.get(0).further_feature + r.body.get(0).Terminal, i);
                    } else {
                        current_select.put(r.body.get(0).Terminal, i);
                    }
                } else if (r.body.get(0).type == 0) {// this body begins with non-Terminal.
                    var current_first = new HashSet<Integer>();
                    current_first.addAll(this.first.get(r.body.get(0).non_Terminal_name));
                    if (current_first.contains(-1)) {
                        HashSet<Integer> next_first = new HashSet<>();
                        int index = 1;
                        while (current_first.contains(-1)) {
                            current_first.remove(-1);
                            next_first.addAll(this.first.get(r.body.get(index++).non_Terminal_name));
                            current_first.addAll(next_first);
                            if (index == this.rules.get(VN).length - 1) {
                                break;
                            }
                        }
                        if (current_first.contains(-1))
                            current_first.addAll(this.follow.get(VN));
                    }
                    for (int t : current_first)
                        current_select.put(t, i);
                } else {// this body is empty(-1)
                    var current_follow = new HashSet<Integer>();
                    current_follow.addAll(this.follow.get(VN));
                    for (int t : current_follow)
                        current_select.put(t, i);
                }
            }
            this.parser_table.put(VN, current_select);
            System.out.println("SELECT(" + VN + ")=" + current_select.toString());
        }
    }

    public LL1_Parser(BufferedReader reader, Lexical_Analysis lexical) throws IOException {
        this.reader = reader;
        this.token_id = new HashMap<>();
        this.rules = new HashMap<>();
        this.Non_Terminals = new ArrayList<>();
        String current_line = "";
        int flag = 0;
        while (true) {
            current_line = reader.readLine();
            if (current_line == null) break;
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
                String head = current_rule[0].substring(1, current_rule[0].length() - 1);
                this.Non_Terminals.add(head);
                this.rules.put(head, current_rules);
            }
        }
        //ok we have read the full grammar, next we need to get the FIRST set and FOLLOW set
        this.first = new HashMap<>();
        get_first_set();
        this.follow = new HashMap<>();
        get_follow_set();
        this.parser_table = new HashMap<>();
        get_parser_table();
    }

    void parse(Token_Sequence token_sequence) {
        var tokens = token_sequence.tokens;
        var stack = new Stack<symbol>();
        stack.push(new symbol(-2, 0));
        stack.push(new symbol(0, 0, "additive_expression"));
        //init the stack: $ Start_Symbol
        int next = 0;
        while (stack.size() != 1) {
            var t = tokens.get(next);
            int type;
            if (t instanceof op_token)
                type = t.type + ((op_token) t).value;
            else
                type = t.type;
            if (stack.peek().type == 0) {//if top is Non-Terminal
                var current_peek = stack.pop();
                int feature = current_peek.feature;
                var map = this.parser_table.get(current_peek.non_Terminal_name);
                //0 <-> normal  1 <-> repeated  2 <-> optional  !3 <-> optional but repeated(only one)  !4 <-> repeated but optional(only one)
                if ((!map.containsKey(type))) {// && (feature != 2 || feature != 3)) {
                    throw new IllegalStateException("Failed to parse! This string is not in the grammar's language!");
                }
                int rule_index = map.get(type);
                rule[] r = this.rules.get(current_peek.non_Terminal_name);
                var body = r[rule_index].body;
                for (int i = body.size() - 1; i >= 0; i--)
                    stack.push(body.get(i));
                //if (feature == 1 || feature == 4)
                //    stack.push(current_peek);
            } else if (stack.peek().type == 1) {//if top is Terminal
                var current_peek = stack.pop();
                if (current_peek.further_feature != -1) {
                    if (current_peek.type + current_peek.further_feature == type) {
                        next++;
                    } else {
                        throw new IllegalStateException("Failed to parse! This string is not in the grammar's language!");
                    }
                } else {
                    if (current_peek.type == type) {
                        next++;
                    } else {
                        throw new IllegalStateException("Failed to parse! This string is not in the grammar's language!");
                    }
                }
            } else {//if top is $
                if (next == tokens.size()) {
                    System.out.println("Success!");
                } else
                    throw new IllegalStateException("Failed to parse! This string is not in the grammar's language!");
            }
        }
    }

    public static void main(String[] args) {
        try {
            var lex_reader = new BufferedReader(new InputStreamReader(new FileInputStream("./test_case/test_add.c")));
            var lexical = new Lexical_Analysis(lex_reader);
            var token_sequence = lexical.scanner();
            System.out.println(token_sequence.toString());
            var grammar_reader = new BufferedReader(new InputStreamReader(new FileInputStream("grammar2.txt")));
            var parser = new LL1_Parser(grammar_reader, lexical);
            parser.parse(token_sequence);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
