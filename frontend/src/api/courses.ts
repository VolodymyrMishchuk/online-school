import { client } from './client';

export interface CourseDto {
    id: string;
    name: string;
    description: string;
    modulesNumber: number;
    status: string;
    isEnrolled?: boolean;
    enrolledAt?: string; // ISO string
}

export interface CreateCourseDto {
    name: string;
    description: string;
    moduleIds?: string[];
}

export interface UpdateCourseDto {
    name: string;
    description: string;
    status: string;
    moduleIds?: string[];
}

export const getCourses = async (userId?: string): Promise<CourseDto[]> => {
    const params = userId ? `?userId=${userId}` : '';
    const response = await client.get(`/courses${params}`);
    return response.data || [];
};

export const createCourse = async (data: CreateCourseDto): Promise<void> => {
    await client.post('/courses', data);
};

export const updateCourse = async (id: string, data: UpdateCourseDto): Promise<void> => {
    await client.put(`/courses/${id}`, data);
};

export const deleteCourse = async (id: string): Promise<void> => {
    await client.delete(`/courses/${id}`);
};
