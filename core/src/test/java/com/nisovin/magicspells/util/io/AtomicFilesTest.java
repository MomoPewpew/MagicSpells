package com.nisovin.magicspells.util.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicFilesTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void createsMissingDirectoriesAndWritesUtf8() throws IOException {
		Path target = temporaryDirectory.resolve("nested/data.txt");

		AtomicFiles.writeUtf8(target, "Zauber ✨\n");

		assertEquals("Zauber ✨\n", Files.readString(target, StandardCharsets.UTF_8));
	}

	@Test
	void replacesExistingFile() throws IOException {
		Path target = temporaryDirectory.resolve("data.txt");
		Files.writeString(target, "old", StandardCharsets.UTF_8);

		AtomicFiles.writeUtf8(target, "new");

		assertEquals("new", Files.readString(target, StandardCharsets.UTF_8));
		assertFalse(hasTemporaryFiles(temporaryDirectory, target));
	}

	@Test
	void supportsEmptyFiles() throws IOException {
		Path target = temporaryDirectory.resolve("data.txt");
		Files.writeString(target, "old", StandardCharsets.UTF_8);

		AtomicFiles.writeUtf8(target, "");

		assertEquals(0, Files.size(target));
	}

	@Test
	void copiesBinaryData() throws IOException {
		Path target = temporaryDirectory.resolve("data.bin");
		byte[] expected = new byte[] { 0, 1, 2, -1 };

		AtomicFiles.copy(target, new ByteArrayInputStream(expected));

		assertArrayEquals(expected, Files.readAllBytes(target));
	}

	@Test
	void keepsExistingFileWhenCopyFails() throws IOException {
		Path target = temporaryDirectory.resolve("data.bin");
		Files.writeString(target, "old", StandardCharsets.UTF_8);
		InputStream failingInput = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("read failed");
			}
		};

		assertThrows(IOException.class, () -> AtomicFiles.copy(target, failingInput));

		assertEquals("old", Files.readString(target, StandardCharsets.UTF_8));
		assertFalse(hasTemporaryFiles(temporaryDirectory, target));
	}

	@Test
	void fallsBackWhenAtomicMoveIsUnsupported() throws IOException {
		Path target = temporaryDirectory.resolve("data.txt");
		Files.writeString(target, "old", StandardCharsets.UTF_8);

		AtomicFiles.writeUtf8(
				target,
				"new",
				(temporary, destination) -> {
					throw new AtomicMoveNotSupportedException(temporary.toString(), destination.toString(), "test");
				},
				Files::deleteIfExists
		);

		assertEquals("new", Files.readString(target, StandardCharsets.UTF_8));
		assertFalse(hasTemporaryFiles(temporaryDirectory, target));
	}

	@Test
	void preservesWriteFailureWhenCleanupAlsoFails() {
		Path target = temporaryDirectory.resolve("data.txt");

		IOException failure = assertThrows(IOException.class, () -> AtomicFiles.writeUtf8(
				target,
				"new",
				(temporary, destination) -> {
					throw new IOException("move failed");
				},
				temporary -> {
					throw new IOException("cleanup failed");
				}
		));

		assertEquals("move failed", failure.getMessage());
		assertEquals(1, failure.getSuppressed().length);
		assertEquals("cleanup failed", failure.getSuppressed()[0].getMessage());
	}

	@Test
	void preservesWriteFailureWhenCleanupFailsUnchecked() {
		Path target = temporaryDirectory.resolve("data.txt");

		IOException failure = assertThrows(IOException.class, () -> AtomicFiles.writeUtf8(
				target,
				"new",
				(temporary, destination) -> {
					throw new IOException("move failed");
				},
				temporary -> {
					throw new SecurityException("cleanup denied");
				}
		));

		assertEquals("move failed", failure.getMessage());
		assertEquals(1, failure.getSuppressed().length);
		assertEquals("cleanup denied", failure.getSuppressed()[0].getMessage());
	}

	@Test
	void removesTemporaryFileWhenReplacementFails() throws IOException {
		Path target = Files.createDirectory(temporaryDirectory.resolve("data.txt"));

		assertThrows(IOException.class, () -> AtomicFiles.writeUtf8(target, "new"));

		assertTrue(Files.isDirectory(target));
		assertFalse(hasTemporaryFiles(temporaryDirectory, target));
	}

	private boolean hasTemporaryFiles(Path directory, Path target) throws IOException {
		String prefix = "." + target.getFileName() + ".";
		try (var files = Files.list(directory)) {
			List<Path> matches = files
					.filter(path -> path.getFileName().toString().startsWith(prefix))
					.toList();
			return !matches.isEmpty();
		}
	}

}
