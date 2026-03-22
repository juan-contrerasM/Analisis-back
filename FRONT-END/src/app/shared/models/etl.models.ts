export type EtlEstado = 'IDLE' | 'EJECUTANDO' | 'LISTO' | 'ERROR';

export interface EtlStatusResponse {
  etlEjecutado: boolean;
  ultimaActualizacion: string | null;
  estado: EtlEstado;
  dataVersion: number;
  mensajeError?: string;
}

export type DatasetRow = Record<string, string | number>;

export interface PatronesActivo {
  subida3: number;
  bajada3: number;
  altaVolatilidad: number;
}

export interface RankingActivo {
  activo: string;
  volatilidad: number;
  riesgo: string;
  patrones: PatronesActivo;
}

export interface AnalysisResponse {
  ranking: RankingActivo[];
}

export interface SimilarityResult {
  euclidiana: number;
  pearson: number;
  coseno: number;
  dtw: number;
  interpretacion: string;
}

export type SeriesResponse = Record<string, number[]>;

export interface EtlNotReadyBody {
  error: string;
  message: string;
}

export type SyncState = 'idle' | 'polling' | 'refreshing' | 'error';
