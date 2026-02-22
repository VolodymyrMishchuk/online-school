import { client } from './client';

export interface Lesson {
    id: string;
    name: string;
    description: string;
    videoUrl?: string;
    moduleId: string;
    moduleName?: string;
    courseName?: string;
    durationMinutes?: number;
    filesCount?: number;
    orderIndex?: number;
    createdBy?: {
        id: string;
        firstName: string;
        lastName: string;
        email: string;
    };
}

export interface CreateLessonDto {
    name: string;
    description: string;
    videoUrl?: string;
    moduleId: string;
    durationMinutes?: number;
}

export const getLessons = async (): Promise<Lesson[]> => {
    const response = await client.get('/lessons');
    return response.data;
};

export const getUnassignedLessons = async (): Promise<Lesson[]> => {
    const response = await client.get('/lessons/unassigned');
    return response.data;
};

export const createLesson = async (data: CreateLessonDto): Promise<Lesson> => {
    const response = await client.post('/lessons', data);
    return response.data;
};

export const getLesson = async (id: string): Promise<Lesson> => {
    const response = await client.get(`/lessons/${id}`);
    return response.data;
};


export const updateLesson = async (id: string, data: Partial<CreateLessonDto>): Promise<void> => {
    await client.put(`/lessons/${id}`, data);
};

export const deleteLesson = async (id: string): Promise<void> => {
    await client.delete(`/lessons/${id}`);
};
