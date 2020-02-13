# Git Workflow

* master is stable
* releases are tagged
* develop is the next
* each development in a dedicated branch, merged into develop with `--no-ff`

# Test Framework

Cf. <https://github.com/jenkinsci/JenkinsPipelineUnit>

Simply run:

    ./gradlew test