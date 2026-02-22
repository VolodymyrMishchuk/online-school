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
}

export const getUsersWithEnrollments = async (): Promise<PersonWithEnrollments[]> => {
    const response = await client.get('/persons/with-enrollments');
    return response.data || [];
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
