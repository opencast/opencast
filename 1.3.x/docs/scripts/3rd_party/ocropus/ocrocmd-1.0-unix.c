#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

#define MAXL 1024
#define MAXC 5000

int main(int argc, char *argv[], char *envp[])
{
  int ii, ac, sts;
  char *av[MAXC];

  ac = 0;
  av[ac] = (char *)malloc(MAXL+1);
  if (av[ac] == NULL) { perror("malloc"); exit(3); }
  strncpy(av[ac], "ocropus", MAXL);
  av[ac++][MAXL] = '\0';

  av[ac] = (char *)malloc(MAXL+1);
  if (av[ac] == NULL) { perror("malloc"); exit(3); }
  strncpy(av[ac], "page", MAXL);
  av[ac++][MAXL] = '\0';

  for (ii = 1; ii < argc && ac < MAXC; ii++) {
    av[ac] = (char *)malloc(MAXL+1);
    if (av[ac] == NULL) { perror("malloc"); exit(3); }
    strncpy(av[ac], argv[ii], MAXL);
    av[ac++][MAXL] = '\0';
  }
  av[ac] = NULL;

  sts = execvp(av[0], av);
  if (sts < 0) { perror(av[0]); exit(1); }

  return 0;
}
