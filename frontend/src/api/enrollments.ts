import { client } from './client';

export interface EnrollmentDto {
    id: string;
    studentId: string;
    courseId: string;
    status: string;
    createdAt?: string; // ISO string
}

export const getMyEnrollments = async (studentId: string): Promise<EnrollmentDto[]> => {
    const response = await client.get(`/enrollments?studentId=${studentId}`);
    return response.data;
};

export const enrollInCourse = async (studentId: string, courseId: string) => {
    return client.post('/enrollments', { studentId, courseId });
};
