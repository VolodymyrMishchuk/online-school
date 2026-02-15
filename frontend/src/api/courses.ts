import { client } from './client';

export interface CourseDto {
    id: string;
    name: string;
    description: string;
    modulesNumber: number;
    status: string;
    price?: number;
    discountAmount?: number;
    discountPercentage?: number;
    accessDuration?: number; // Days
    promotionalDiscountPercentage?: number;
    promotionalDiscountAmount?: number;
    nextCourseId?: string;
    nextCourseName?: string;
    isEnrolled?: boolean;
    enrolledAt?: string; // ISO string
    enrollmentStatus?: 'ACTIVE' | 'EXPIRED' | 'BLOCKED' | 'PENDING';
    expiresAt?: string;
}

export interface CreateCourseDto {
    name: string;
    description: string;
    price?: number;
    discountAmount?: number;
    discountPercentage?: number;
    accessDuration?: number;
    promotionalDiscountPercentage?: number;
    promotionalDiscountAmount?: number;
    nextCourseId?: string;
    moduleIds?: string[];
}

export interface UpdateCourseDto {
    name: string;
    description: string;
    status: string;
    price?: number;
    discountAmount?: number;
    discountPercentage?: number;
    accessDuration?: number;
    promotionalDiscountPercentage?: number;
    promotionalDiscountAmount?: number;
    nextCourseId?: string;
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

export const extendAccessForReview = async (courseId: string, video: File): Promise<void> => {
    const formData = new FormData();
    formData.append('video', video);
    await client.post(`/courses/${courseId}/extend-access`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};
