export type NotificationType = "success" | "error" | "info" | "loading";

export type NotificationPoliteness = "polite" | "assertive";

export type ToastNotification = {
  id: string;
  type: NotificationType;
  message: string;
  createdAt: number;
  durationMs: number | null;
  politeness: NotificationPoliteness;
};

export const DEFAULT_NOTIFICATION_POLICY = {
  duration: {
    success: 4200,
    error: 6200,
    info: 5000,
    loading: null,
  },
  fieldValidation: {
    globalToastFallback: false,
    guidance: "Keep field validation errors next to the related field.",
  },
} as const;

type NotificationListener = () => void;

const listeners = new Set<NotificationListener>();

let notifications: ToastNotification[] = [];

function createId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `toast-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function emit() {
  listeners.forEach(listener => listener());
}

function getPoliteness(type: NotificationType): NotificationPoliteness {
  return type === "error" ? "assertive" : "polite";
}

function addNotification(type: NotificationType, message: string) {
  const id = createId();
  const durationMs = DEFAULT_NOTIFICATION_POLICY.duration[type];

  notifications = [
    ...notifications,
    {
      id,
      type,
      message,
      createdAt: Date.now(),
      durationMs,
      politeness: getPoliteness(type),
    },
  ];

  emit();

  return id;
}

function dismissNotification(id: string) {
  const nextNotifications = notifications.filter(notification => notification.id !== id);

  if (nextNotifications.length !== notifications.length) {
    notifications = nextNotifications;
    emit();
  }
}

function clearNotifications() {
  notifications = [];
  emit();
}

export function subscribeToNotifications(listener: NotificationListener) {
  listeners.add(listener);

  return () => {
    listeners.delete(listener);
  };
}

export const notify = {
  success(message: string) {
    return addNotification("success", message);
  },
  error(message: string) {
    return addNotification("error", message);
  },
  info(message: string) {
    return addNotification("info", message);
  },
  loading(message: string) {
    return addNotification("loading", message);
  },
  dismiss(id: string) {
    dismissNotification(id);
  },
  clear() {
    clearNotifications();
  },
  getSnapshot() {
    return [...notifications];
  },
};
