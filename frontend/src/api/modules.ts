import { client } from './client';

export interface Module {
    id: string;
    name: string;
    description: string;
    courseId?: string;
    courseName?: string;
    orderIndex?: number;
    lessonsNumber?: number;
    durationMinutes?: number;
}

export interface CreateModuleDto {
    name: string;
    description: string;
    courseId: string;
    lessonIds?: string[]; // Optional: lessons to assign to this module
}

export const getModules = async (courseId?: string): Promise<Module[]> => {
    const params = courseId ? `?courseId=${courseId}` : '';
    const response = await client.get(`/modules${params}`);
    return response.data;
};

export const getModule = async (id: string): Promise<Module> => {
    const response = await client.get(`/modules/${id}`);
    return response.data;
};

import type { Lesson } from './lessons';

export const getModuleLessons = async (moduleId: string): Promise<Lesson[]> => {
    const response = await client.get(`/modules/${moduleId}/lessons`);
    return response.data;
};

export const createModule = async (data: CreateModuleDto): Promise<Module> => {
    const response = await client.post('/modules', data);
    return response.data;
};

export const updateModule = async (id: string, data: Partial<CreateModuleDto>): Promise<void> => {
    await client.put(`/modules/${id}`, data);
};

export const deleteModule = async (id: string): Promise<void> => {
    await client.delete(`/modules/${id}`);
};
