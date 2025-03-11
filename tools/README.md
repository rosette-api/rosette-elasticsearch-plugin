# Releasing the Elasticsearch plugin

## Requirements

To release you need:
- an OSSRH account
- gpg installed and a pgp key published
- credentials for the above added to your maven settings file
- personal OAuth access token with full repo permissions

The process for this is described [here](https://github.com/RosetteTextAnalytics/rosapi1.5/blob/master/doc/release-binding.md#request-access-to-ossrh-if-not-already-done).

## Releasing

A single script takes care of running the maven release process as well as creating the release in github.

From the root directory of the repo run:
`./tools/release.sh ${ELASTIC_VERSION} ${GITHUB_ACCESS_TOKEN}`

As an example:
`./tools/release.sh 5.6.2 012345abcdef`

If successful head to https://oss.sonatype.org/#stagingRepositories to release the staged artifacts.
