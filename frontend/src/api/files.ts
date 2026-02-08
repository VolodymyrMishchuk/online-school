import { client } from './client';

export interface FileDto {
    id: string;
    fileName: string;
    originalName: string;
    contentType: string;
    fileSize: number;
    uploadedAt: string;
    downloadUrl?: string;
}

export const uploadFile = async (
    file: File,
    entityType: string,
    entityId: string
): Promise<FileDto> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('entityType', entityType);
    formData.append('entityId', entityId);

    const response = await client.post('/api/files/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

export const getLessonFiles = async (lessonId: string): Promise<FileDto[]> => {
    const response = await client.get(`/lessons/${lessonId}/files`);
    return response.data;
};

export const downloadFile = async (fileId: string): Promise<Blob> => {
    // Only use the ID, ignore full URL if passed to keep it clean
    const id = fileId.includes('/') ? fileId.split('/').pop() || fileId : fileId;

    const response = await client.get(`/api/files/${id}`, {
        responseType: 'blob',
    });
    return response.data;
};

export const deleteFile = async (fileId: string): Promise<void> => {
    await client.delete(`/api/files/${fileId}`);
};
