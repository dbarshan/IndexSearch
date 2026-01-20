# IndexSearch

IndexSearch is a Spring Boot based application that implements an **Inverted Index** for efficient document searching. It supports creating, updating, deleting, and searching documents via a RESTful API.

## Features

- **Inverted Indexing**: Fast full-text search capabilities.
- **REST API**: Simple interface for document management.
- **Persistence**: Persists data to the local disk (`data/` directory) to survive restarts.
- **Spring Boot 3**: Built on the latest modern Java stack.

## Prerequisites

- **Java 17** or higher
- **Maven** 3.6+

## Getting Started

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd IndexSearch
   ```

2. Build the application:
   ```bash
   mvn clean install
   ```

### Running the Application

Run the application using the Spring Boot Maven plugin:
```bash
mvn spring-boot:run
```

Or run the built JAR file directly:
```bash
java -jar target/indexsearch-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`.

## API Usage

The application provides a REST API at `/api`.

### 1. Add a Document
**endpoint**: `POST /api/documents`

```bash
curl -X POST "http://localhost:8080/api/documents?keyField=content" \
     -H "Content-Type: application/json" \
     -d '{"id": "doc1", "title": "My First Doc", "content": "hello world search"}'
```

### 2. Search Documents
**endpoint**: `GET /api/search`

```bash
curl -s "http://localhost:8080/api/search?query=hello" | jq .
```

### 3. Update a Document
**endpoint**: `PUT /api/documents/{id}`

```bash
curl -X PUT "http://localhost:8080/api/documents/doc1?keyField=content" \
     -H "Content-Type: application/json" \
     -d '{"title": "Updated Title", "content": "hello universe search"}'
```

### 4. Delete a Document
**endpoint**: `DELETE /api/documents/{id}`

```bash
curl -X DELETE "http://localhost:8080/api/documents/doc1"
```

## Testing & Verification

The project includes shell scripts to valid functionality:

- **`test_endpoints.sh`**: Runs a sequence of API calls to test CRUD operations and search.
- **`verify_persistence.sh`**: Tests that documents are correctly saved to disk and can be retrieved after an application restart.

Run them using:
```bash
./test_endpoints.sh
./verify_persistence.sh
```
