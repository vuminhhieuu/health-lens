import { z } from 'zod';
import { GENDER_OPTIONS } from '../constants';

const optionalPersonalText = (max: number, label: string) =>
  z
    .string()
    .max(max, `${label} tối đa ${max} ký tự`)
    .optional()
    .or(z.literal(''))
    .or(z.null());

const optionalClinicalText = (label: string) =>
  z
    .string()
    .max(1000, `${label} tối đa 1000 ký tự`)
    .optional()
    .or(z.literal(''))
    .or(z.null());

export const updateProfileSchema = z.object({
  fullName: z.string().min(1, 'Họ tên không được để trống').max(120, 'Họ tên quá dài'),
  birthDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Định dạng YYYY-MM-DD').optional().or(z.literal('')),
  gender: z.enum(GENDER_OPTIONS as unknown as [string, ...string[]]).optional().or(z.literal('')),
  personalDescription: optionalPersonalText(500, 'Mô tả cá nhân'),
  personalNotes: optionalPersonalText(500, 'Ghi chú cá nhân'),
});

export type UpdateProfileInput = z.infer<typeof updateProfileSchema>;

export const updateUserProfileSchema = updateProfileSchema;
export type UpdateUserProfileInput = UpdateProfileInput;

export const updateHealthContextSchema = z.object({
  chronicConditions: optionalClinicalText('Bệnh nền'),
  currentMedications: optionalClinicalText('Thuốc đang dùng'),
  allergies: optionalClinicalText('Dị ứng'),
});

export type UpdateHealthContextInput = z.infer<typeof updateHealthContextSchema>;
