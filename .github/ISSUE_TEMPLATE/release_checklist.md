---
name: Release Checklist
about: A release-prep checklist
title: "Release Checklist"
labels:
  - release
assignees: []
hidden: true    # do NOT show in template picker every time someone creates a new issue
---

## Release checklist

> [!TIP]
> Create an issue from this template [using this link](https://github.com/DataONEorg/dataone-indexer/issues/new?template=release_checklist.md)

### CHOOSE ONE, AND DELETE THE OTHER:

---
### EITHER:

### Release of Software & helm Chart Together

- [ ] Create a branch named `feature-<issueNum>-<releaseVer>-release-prep`, and do the following:
  - [ ] **pom.xml**: Update `<version>x.x.x</version>`
  - [ ] **Chart.yaml**: Update chart `version` and `appVersion`
  - [ ] Grep codebase for previous release number, just in case
  - [ ] **RELEASE-NOTES.md**:
    - [ ] Update for new app & chart versions
    - [ ] DON'T FORGET TO SET CORRECT RELEASE DATE!
  - [ ] PR & merge release prep branch to `develop`
- [ ] PR & merge `develop` -> `main`
- [ ] **(on Mac)** package and push helm chart
- [ ] **(on Mac)** build jar & `mvn deploy` to maven repo
- [ ] Tag the release; look up the `<commit-sha>` from `git log`, then:
  ```shell
  git tag x.x.x <commit-sha>
  git tag chart-x.x.x <commit-sha>
  git push --tags    ## IMPORTANT - DON'T FORGET THIS!
  ```
- [ ] Verify that the GH Action successfully built and pushed docker image with version==release tag
- [ ] Add to GH `Releases` page
- [ ] Announce on Slack?

---
### OR:

### Release of helm Chart Only

- [ ] `git checkout <TAG NUMBER OF EXISTING RELEASE>`
- [ ] create branch from this tag: `feature-<issueNum>-<releaseVer>-release-prep`
  - [ ] Chart.yaml: Update chart version and any other details necessary
  - [ ] grep for previous release number, just in case
  - [ ] RELEASE-NOTES.md:
    - [ ] Update for new version(s).
    - [ ] DON'T FORGET TO SET CORRECT RELEASE DATE!
  - [ ] `git cherry-pick` any commits that need to be included from develop
  - [ ] PR & merge to develop
  - [ ] PR & merge to main
- [ ] **(Mac)** package and push helm chart
  ```shell
  helm package -u ./helm
  
  helm push dataone-indexer-<x.x.x>.tgz oci://ghcr.io/dataoneorg/charts
  ```
- [ ] Tag the release; look up the `<commit-sha>` from `git log`, then:
  ```shell
  git tag chart-x.x.x <commit-sha>
  git push --tags    ## IMPORTANT - DON'T FORGET THIS!
  ```
- [ ] Add to GH `Releases` page
- [ ] Announce on Slack?
