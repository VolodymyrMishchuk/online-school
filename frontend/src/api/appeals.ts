import api from './client';

export interface AppealCreateRequest {
    contactMethod: 'MOBILE' | 'INSTAGRAM' | 'TELEGRAM' | 'EMAIL';
    contactDetails: string;
    message: string;
}

export interface FileDto {
    id: string;
    fileName: string;
    originalName: string;
    contentType: string;
    fileSize: number;
    downloadUrl?: string; // or handled dynamically
}

export interface AppealResponse {
    id: string;
    userId: string;
    userFirstName: string;
    userLastName: string;
    userEmail: string;
    contactMethod: 'MOBILE' | 'INSTAGRAM' | 'TELEGRAM' | 'EMAIL';
    contactDetails: string;
    message: string;
    status: 'NEW' | 'PROCESSED';
    createdAt: string;
    photos: FileDto[];
}

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
}

export const createAppeal = async (formData: FormData): Promise<AppealResponse> => {
    const response = await api.post<AppealResponse>('/appeals', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

export const getAppeals = async (page = 0, size = 20): Promise<PageResponse<AppealResponse>> => {
    const response = await api.get<PageResponse<AppealResponse>>('/appeals', {
        params: { page, size },
    });
    return response.data;
};

export const getAppeal = async (id: string): Promise<AppealResponse> => {
    const response = await api.get<AppealResponse>(`/appeals/${id}`);
    return response.data;
};

export const updateAppealStatus = async (id: string, status: 'NEW' | 'PROCESSED'): Promise<AppealResponse> => {
    const response = await api.patch<AppealResponse>(`/appeals/${id}/status`, null, {
        params: { status },
    });
    return response.data;
};

export const deleteAppeal = async (id: string): Promise<void> => {
    await api.delete(`/appeals/${id}`);
};
