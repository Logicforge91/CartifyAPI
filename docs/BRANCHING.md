# Git branching strategy

CartifyAPI uses short-lived feature branches with controlled promotion through long-lived environment branches.

## Branches

- `main`: production-ready code. Merge only tested release changes through pull requests.
- `staging`: release-candidate integration and acceptance testing.
- `testing`: shared QA and automated integration testing.
- `develop`: completed features awaiting promotion to testing.
- `feature/<ticket>-<description>` and `fix/<ticket>-<description>`: short-lived work branches created from `develop`.
- `hotfix/<ticket>-<description>`: urgent production fixes created from `main` and merged back into `main` and `develop`.

## Promotion flow

```text
feature/fix -> develop -> testing -> staging -> main
```

Every promotion uses a pull request, a passing Maven build, and at least one review. Direct pushes to long-lived branches should be disabled with repository branch protection. Delete short-lived branches after merge.

Production releases from `main` should be tagged using semantic versions such as `v1.2.0`.
