app:
  name: bff
  image:
    repository: "onecx/${artifactId}"
  operator:
    permission:
      enabled: true
    microservice:
      spec:
        description: ${projectName}
        name: ${projectName}

