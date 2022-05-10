#include <stdio.h>

void f(int x, int y) {
    int z = x;
    x = 2;
    {
        float x;
        x = 3.5;
        z = (int)x;
    }
    z = (int)x;
}
int main() {
    printf("Hello world\n");
    float a = 1.2e10;
    char b = 'b';
    int c = 2 * 4;
    if (a > 1.3e5) {
        printf("%lf", a);
    }
    return 0;
}
