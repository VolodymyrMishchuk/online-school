import { client } from './client';

export interface CourseDto {
    id: string;
    name: string;
    description: string;
    modulesNumber: number;
    status: string;
}

export const getCourses = async (): Promise<CourseDto[]> => {
    const response = await client.get('/courses');
    return response.data;
};
