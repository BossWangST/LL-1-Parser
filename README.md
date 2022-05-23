# LL-1-Parser
## Main Ideas
Since we need to construct a LL(1) Parser, which is a Table-Driving Parser, the ultimate object of the program then is to finally construct a 
LL(1) parser table. 

In order to get the final parser table, we should follow the following steps:
### Step1:Read the grammar into the program

We know that the LL(1) parser needs a LL(1) grammar to parse codes, so firstly it's important to construct a structure to describe the grammar.
Due to the weakness of LL(1), which are Left-recursions and ambiguous rules, when writing the grammar, it must be a LL(1) grammar.

After that, we can then move to step 2.

### Step2:Get the FIRST & FOLLOW set 

According to the *Compilers: Principles, Techniques, and Tools* ,the algorithm to get FIRST and FOLLOW set is clear, but we need to really implement the algorithm in JAVA, then it's essential to use a lot JAVA data structures efficiently. You can see the usage in the code.

### Step3:Get the SELECT set 

We know that, to construct a LL(1) parser table, we need to calculate the SELECT set to help us build the parser table easily. 

### Step4:Get the Token Sequence using Scanner 

To parse codes, we should firstly get the Token Sequence of the codes, which we can get from the Scanner program. The Scanner program created by Lex has been pack into a JAR file, so we can 
import the JAR file into our LL(1) Parser program, then just call the `scanner()` function.

### Step5:Parse the codes 

Parsing codes is a process of using the stack to check the parser table. If finally out stack's top is $ while we have read all the tokens to the end, we can then declare that the code is in the language of the grammar!

此时，成功！（感谢姜老师在LL(1)部分的细致讲解！）