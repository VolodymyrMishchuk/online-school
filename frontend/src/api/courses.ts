import { client } from './client';

export interface CourseDto {
    id: string;
    name: string;
    description: string;
    modulesNumber: number;
    lessonsCount?: number;
    durationMinutes?: number;
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
    coverImageUrl?: string;
    averageColor?: string;
    createdBy?: {
        id: string;
        firstName: string;
        lastName: string;
        email: string;
    };
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
    deleteCoverImage?: boolean;
}

export const getCourses = async (userId?: string): Promise<CourseDto[]> => {
    const params = userId ? `?userId=${userId}` : '';
    const response = await client.get(`/api/courses${params}`);
    return response.data || [];
};

export const getCourse = async (id: string): Promise<CourseDto> => {
    const response = await client.get(`/api/courses/${id}`);
    return response.data;
};

export const createCourse = async (data: CreateCourseDto, image?: File): Promise<void> => {
    const formData = new FormData();
    formData.append('course', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (image) {
        formData.append('image', image);
    }
    await client.post('/api/courses', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

export const updateCourse = async (id: string, data: UpdateCourseDto, image?: File): Promise<void> => {
    const formData = new FormData();
    formData.append('course', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (image) {
        formData.append('image', image);
    }
    await client.put(`/api/courses/${id}`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

export const deleteCourse = async (id: string): Promise<void> => {
    await client.delete(`/api/courses/${id}`);
};

export const extendAccessForReview = async (courseId: string, video: File): Promise<void> => {
    const formData = new FormData();
    formData.append('video', video);
    await client.post(`/api/courses/${courseId}/extend-access`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};
