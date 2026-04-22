import { z } from 'zod';
import { GENDER_OPTIONS } from '../constants';

export const updateProfileSchema = z.object({
  fullName: z.string().min(1, 'Họ tên không được để trống').max(120, 'Họ tên quá dài'),
  birthDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Định dạng YYYY-MM-DD').optional().or(z.literal('')),
  gender: z.enum(GENDER_OPTIONS as unknown as [string, ...string[]]).optional().or(z.literal('')),
});

export type UpdateProfileInput = z.infer<typeof updateProfileSchema>;

export const updateUserProfileSchema = updateProfileSchema;
export type UpdateUserProfileInput = UpdateProfileInput;
