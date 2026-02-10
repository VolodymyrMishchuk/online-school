import { client } from './client';

export interface PersonDto {
    id: string;
    firstName: string;
    lastName: string;
    bornedAt: string; // ISO string
    phoneNumber: string;
    email: string;
    role: string;
    status: string;
    createdAt: string;
    updatedAt: string;
}

export interface PersonUpdateDto {
    firstName?: string;
    lastName?: string;
    bornedAt?: string; // ISO string or null
    phoneNumber?: string;
    email?: string; // Optional, usually strictly controlled
    password?: string; // Optional
    role?: string; // Optional
    status?: string; // Optional
}

export const getPerson = async (id: string): Promise<PersonDto> => {
    const response = await client.get<PersonDto>(`/persons/${id}`);
    return response.data;
};

export const updatePerson = async (id: string, data: PersonUpdateDto): Promise<void> => {
    await client.put(`/persons/${id}`, data);
};
