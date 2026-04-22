import { z } from "zod";
import { GENDER_OPTIONS, ProfileGender } from "../constants";

export const createProfileSchema = z.object({
  displayName: z
    .string()
    .min(1, "Tên hiển thị không được để trống")
    .max(50, "Tên hiển thị quá dài"),
  birthDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Định dạng YYYY-MM-DD")
    .optional()
    .or(z.literal(""))
    .or(z.null()),
  gender: z
    .enum(GENDER_OPTIONS as unknown as [ProfileGender, ...ProfileGender[]])
    .optional()
    .or(z.literal(""))
    .or(z.null()),
  notes: z
    .string()
    .max(1000, "Ghi chú quá dài")
    .optional()
    .or(z.literal(""))
    .or(z.null()),
});

export type CreateProfileInput = z.infer<typeof createProfileSchema>;

export const updateProfileSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, "Tên không được để trống")
    .max(100, "Tên tối đa 100 ký tự"),
  notes: z
    .string()
    .max(500, "Ghi chú tối đa 500 ký tự")
    .optional()
    .or(z.literal(""))
    .or(z.null()),
  birthDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Định dạng YYYY-MM-DD")
    .optional()
    .or(z.literal(""))
    .or(z.null()),
  gender: z
    .enum(GENDER_OPTIONS as unknown as [ProfileGender, ...ProfileGender[]])
    .optional()
    .or(z.literal(""))
    .or(z.null()),
});

export type UpdateProfileInput = z.infer<typeof updateProfileSchema>;
