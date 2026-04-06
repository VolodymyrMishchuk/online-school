import { client } from './client';

export interface EnrollmentDto {
    id: string;
    studentId: string;
    courseId: string;
    courseName?: string;
    status: string;
    createdAt?: string;
    updatedAt?: string;
}

export interface PersonWithEnrollments {
    id: string;
    firstName: string;
    lastName: string;
    bornedAt: string;
    phoneNumber: string;
    email: string;
    role: string;
    status: string;
    language: string;
    avatarUrl?: string;
    enrollments: EnrollmentDto[];
    createdAt?: string;
    updatedAt?: string;
    createdBy?: {
        id: string;
        firstName: string;
        lastName: string;
        email: string;
    };
}

export interface CreatePersonDto {
    firstName: string;
    lastName: string;
    email: string;
    password?: string;
    phoneNumber: string;
    bornedAt?: string;
    role: string;
    language?: string;
    courseIds?: string[];
}

export interface UpdatePersonDto {
    firstName: string;
    lastName: string;
    phoneNumber?: string;
    bornedAt?: string;
    role?: string;
    status?: string;
    email?: string;
    language?: string;
}

export const getUsersWithEnrollments = async (): Promise<PersonWithEnrollments[]> => {
    const response = await client.get('/persons/with-enrollments');
    return response.data || [];
};

export interface PaginatedResponse<T> {
    content: T[];
    pageable: any;
    last: boolean;
    totalElements: number;
    totalPages: number;
    first: boolean;
    size: number;
    number: number;
    sort: any;
    numberOfElements: number;
    empty: boolean;
}

export const getPaginatedUsers = async (
    page: number = 0,
    size: number = 20,
    search?: string,
    sortKey?: string | null,
    sortDir?: 'asc' | 'desc' | null,
    blockedSort?: 'top' | 'bottom' | null,
    adminSort?: 'top' | 'bottom' | null
): Promise<PaginatedResponse<PersonWithEnrollments>> => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());

    if (search) params.append('search', search);
    if (sortKey) params.append('sortKey', sortKey);
    if (sortDir) params.append('sortDir', sortDir);
    if (blockedSort) params.append('blockedSort', blockedSort);
    if (adminSort) params.append('adminSort', adminSort);

    const response = await client.get(`/persons/paginated?${params.toString()}`);
    return response.data;
};

export const updatePersonStatus = async (id: string, status: string): Promise<void> => {
    await client.patch(`/persons/${id}/status?status=${status}`);
};

export const addCourseAccess = async (personId: string, courseId: string): Promise<void> => {
    await client.post(`/persons/${personId}/enrollments/${courseId}`);
};

export const removeCourseAccess = async (personId: string, courseId: string): Promise<void> => {
    await client.delete(`/persons/${personId}/enrollments/${courseId}`);
};

export const createPerson = async (data: CreatePersonDto): Promise<void> => {
    await client.post('/persons', data);
};

export const updatePerson = async (id: string, data: UpdatePersonDto): Promise<void> => {
    await client.put(`/persons/${id}`, data);
};

export const deletePerson = async (id: string): Promise<void> => {
    await client.delete(`/persons/${id}`);
};

export const getRoles = async (): Promise<string[]> => {
    const response = await client.get('/references/roles');
    return response.data;
};

export const getStatuses = async (): Promise<string[]> => {
    const response = await client.get('/references/statuses');
    return response.data;
};
