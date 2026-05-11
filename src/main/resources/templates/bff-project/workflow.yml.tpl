name: ${workflowName}
${triggerBlock}
jobs:
  ${jobName}:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '${javaVersion}'
      - name: Run workflow command
        run: ${runCommand}
