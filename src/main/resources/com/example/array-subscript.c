#include <stdio.h>

int main(int argc, char *argv[]) {
    int integers[] = {1, 2, 3, 4, 5};

    int first = integers[0];
    printf("%d\n", first);

    const int second = integers[1];
    printf("%d\n", second);

    int third = (int) integers[2];
    printf("%d\n", third);

    printf("%d\n", integers[3]);

    printf("%d\n", 4[integers]);

    const char *string = "ABCD";
    printf("%c\n", string[0]);

    printf("%c\n", 1[string]);

    printf("%c\n", "ABCD"[2]);

    printf("%c\n", 3["ABCD"]);

    return 0;
}
