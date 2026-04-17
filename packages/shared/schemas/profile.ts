import { z } from 'zod';
import { GENDER_OPTIONS, Gender } from '../constants';

export const createProfileSchema = z.object({
  displayName: z.string()
    .min(1, 'Tên hiển thị không được để trống')
    .max(50, 'Tên hiển thị quá dài'),
  birthDate: z.string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Định dạng YYYY-MM-DD')
    .optional()
    .or(z.literal(''))
    .or(z.null()),
  gender: z.enum(GENDER_OPTIONS as unknown as [Gender, ...Gender[]]).optional()
    .or(z.literal(''))
    .or(z.null()),
  notes: z.string().max(1000, 'Ghi chú quá dài').optional().or(z.literal('')).or(z.null()),
});

export type CreateProfileInput = z.infer<typeof createProfileSchema>;
