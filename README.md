# loc — Lines of Code Utilities

Three command-line tools for analysing contribution history in a Git repository.

- **loc-authors** — list all authors who have ever committed to the repository
- **loc-authorship** — count lines currently attributed to an author via `git blame`
- **loc-by-user** — show lines added/deleted per month by an author via `git log`

## Building

```sh
mvn package
```

This produces three executables in the project root: `loc-authors`, `loc-authorship`, and `loc-by-user`.

## Usage

### 1. Find all authors in a repository

```sh
./loc-authors -r /path/to/repo
```

Example output:

```
Alice Example
Bob Example
Joe Bloggs
```

### 2. Check current line ownership for an author

```sh
./loc-authorship -r /path/to/repo -n "Joe Bloggs"
```

### 3. Show monthly additions and deletions for an author

```sh
./loc-by-user -r /path/to/repo -n "Joe Bloggs"
```

## Putting it together

Use `loc-authors` to discover the exact author name, then feed it into the other tools:

```sh
# List all authors
./loc-authors -r /path/to/repo

# Pick a name from the output and run both tools against it
./loc-authorship -r /path/to/repo -n "Joe Bloggs"
./loc-by-user -r /path/to/repo -n "Joe Bloggs"
```
