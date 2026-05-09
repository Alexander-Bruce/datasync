package backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.exception.model.BaseException;
import backend.mapper.sqlite.FileMapper;
import backend.mapper.sqlite.UserMapper;
import backend.model.entity.File;
import backend.model.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FileServiceImplTest {

  private static final String EMAIL = "user@example.com";

  @Test
  void updateFileTaskRejectsDuplicateAliasForSameUser() {
    FileServiceImpl service = new FileServiceImpl();
    service.userMapper = mock(UserMapper.class);
    service.fileMapper = mock(FileMapper.class);

    when(service.userMapper.selectByEmail(EMAIL))
        .thenReturn(User.builder().id(42).email(EMAIL).build());
    when(service.fileMapper.countByAliasForUser("Task A", 42, null)).thenReturn(1);

    BaseException ex =
        assertThrows(
            BaseException.class,
            () ->
                service.updateFileTask(
                    0, " Task A ", "C:\\data\\same-path", EMAIL, "", "FastCDC", "", "", true));

    assertEquals(409, ex.getCode());
    verify(service.fileMapper, never()).insert(any(File.class));
  }

  @Test
  void updateFileTaskAllowsSamePathWhenAliasIsUnique() {
    FileServiceImpl service = new FileServiceImpl();
    service.userMapper = mock(UserMapper.class);
    service.fileMapper = mock(FileMapper.class);

    File original =
        File.builder().id(7).alias("Old Task").path("C:\\data\\same-path").userId(42).build();
    File saved =
        File.builder().id(7).alias("New Task").path("C:\\data\\same-path").userId(42).build();

    when(service.userMapper.selectByEmail(EMAIL))
        .thenReturn(User.builder().id(42).email(EMAIL).build());
    when(service.fileMapper.selectByFileId(7)).thenReturn(original, saved);
    when(service.fileMapper.countByAliasForUser("New Task", 42, 7)).thenReturn(0);

    File result =
        service.updateFileTask(
            7, " New Task ", "C:\\data\\same-path", EMAIL, "", "FastCDC", "", "", true);

    ArgumentCaptor<File> updatedFile = ArgumentCaptor.forClass(File.class);
    verify(service.fileMapper).update(updatedFile.capture());
    verify(service.fileMapper, never()).selectByPath(anyString(), anyInt());

    assertEquals(7, result.getId());
    assertEquals("New Task", updatedFile.getValue().getAlias());
    assertEquals("C:\\data\\same-path", updatedFile.getValue().getPath());
  }
}
