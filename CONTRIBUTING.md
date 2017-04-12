# Contributing

For those who wish to contribute to this project, here are some guidelines.

Fork, then clone the repo:

    git clone git@github.com:your-username/rosette-elasticsearch-plugin.git

Set up your environment:

- Java 8
- Maven
- Get a [Rosette API key](https://developer.rosette.com/signup) if you don't have one
- export ROSETTE_API_KEY="\<your key\>"

Make sure you can successfully build and tests pass:

    mvn clean install

Make your change. Add tests for your change. Make sure everything still passes:

    mvn clean install

Push to your fork and [submit a pull request][pr].

[pr]: https://github.com/rosette-api/rosette-elasticsearch-plugin/compare/

We'll take a look and may suggest some changes or improvements or alternatives.

To increase the chance that your pull request is accepted, make sure to write tests, clean commented code, and [good commit messages][commit].

[commit]: http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
