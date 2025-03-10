// 글조각 관련 API를 호출하는 함수 모음
import axiosAPI from "./axios";

export interface Image {
  imageId: number;
  imageUrl: string;
}

/**
 * 스토리(글조각) 정보 인터페이스
 */
export interface Story {
  id: number;
  title: string;
  content: string;
  createdAt: Date;
  updatedAt: Date;
  eventId: number;
  occurredAt: Date;
  images: string[];
}

/**
 * 스토리 생성 요청 데이터 인터페이스
 */
export interface StoryDto {
  userId: string;
  title: string;
  content: string;
  eventId: number;
}

/**
 * 특정 사용자의 모든 스토리를 가져오는 함수
 * @returns Promise<Story[]> - 사용자의 모든 스토리 목록 반환
 */
export const getAllStories = async (): Promise<Story[]> => {
  try {
    const response = await axiosAPI.get("/stories");

    return response.data;
  } catch (error) {
    throw new Error("getAllStories 에러 발생");
  }
};

/**
 * 새로운 스토리를 생성하는 함수
 * @param title - 스토리 제목
 * @param content - 스토리 내용
 * @param eventId - 관련 이벤트 ID
 * @param images - 업로드할 이미지 목록
 * @returns Promise<number> - 생성된 스토리의 ID 반환
 */
export const makeStory = async (
  title: string,
  content: string,
  eventId: number,
  images: File[]
): Promise<number> => {
  try {
    const formData = new FormData();
    formData.append(
      "storyDto",
      new Blob([JSON.stringify({ title, content, eventId })], {
        type: "application/json",
      })
    );

    images.forEach((image) => {
      formData.append("images", image);
    });

    const response = await axiosAPI.post("/stories", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    return response.data;
  } catch (error: any) {
    const errorMessage =
      error.response.data.message ||
      "스토리 생성 중 알 수 없는 에러가 발생했습니다.";
    alert(errorMessage);
    return Promise.reject(errorMessage);
  }
};

/**
 * 특정 ID의 스토리를 조회하는 함수
 * @param storyId - 조회할 스토리의 ID
 * @returns Promise<Story> - 조회된 스토리 객체 반환
 */
export const getStoryById = async (storyId: number): Promise<Story> => {
  try {
    const response = await axiosAPI.get(`/stories/${storyId}`);

    return response.data;
  } catch (error) {
    throw new Error("getStoryByStoryId 에러 발생!");
  }
};

/**
 * 특정 ID의 스토리를 삭제하는 함수
 * @param storyId - 삭제할 스토리의 ID
 * @returns Promise<boolean> - 삭제 성공 여부 반환
 */
export const deleteStory = async (storyId: number): Promise<boolean> => {
  try {
    const response = await axiosAPI.delete(`/stories/${storyId}`);

    if (response.status === 200) {
      return true;
    } else {
      return false;
    }
  } catch (error) {
    throw new Error("deleteStory 에러 발생");
  }
};

/**
 * 기존 스토리를 업데이트하는 함수
 * @param storyId - 수정할 스토리의 ID
 * @param title - 수정할 제목
 * @param content - 수정할 내용
 * @param eventId - 관련 이벤트 ID
 * @param deleteImageIds - 삭제할 이미지 ID
 * @param images - 업데이트할 이미지 목록
 * @returns Promise<Story> - 업데이트된 스토리 객체 반환
 */
export const updateStory = async (
  storyId: number,
  title: string,
  content: string,
  eventId: number,
  deleteImageIds: String[],
  images: File[]
): Promise<Story> => {
  try {
    const formData = new FormData();

    const storyDto = JSON.stringify({
      title,
      content,
      eventId,
      deleteImageIds,
    });
    formData.append(
      "storyDto",
      new Blob([storyDto], { type: "application/json" })
    );

    images.forEach((image) => {
      formData.append("images", image);
    });

    const response = await axiosAPI.patch(`/stories/${storyId}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    return response.data;
  } catch (error) {
    throw new Error(`updateStory 에러 발생! storyId : ${storyId}`);
  }
};
